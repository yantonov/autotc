var Nav = ReactBootstrap.Nav;
var NavItem = ReactBootstrap.NavItem;
var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var ListGroup = ReactBootstrap.ListGroup;
var ListGroupItem = ReactBootstrap.ListGroupItem;
var Input = ReactBootstrap.Input;
var ButtonToolbar = ReactBootstrap.ButtonToolbar;
var Button = ReactBootstrap.Button;
var Panel = ReactBootstrap.Panel;
var Glyphicon = ReactBootstrap.Glyphicon;
var Modal = ReactBootstrap.Modal;

var ServerList = React.createClass({
    onSelect: function(serverIndex) {
        this.props.onServerSelect(serverIndex);
    },
    render: function() {
        var servers = this.props.servers.map(function (server, index) {
            return (
                <NavItem eventKey={index} href="#">{server.alias}</NavItem>
            );
        });
        return (
            <Nav bsStyle="tabs" activeKey={this.props.selectedServerIndex} onSelect={this.onSelect}>
            {servers}
            </Nav>
        );
    }
});

var AgentStatus = React.createClass({
    getImage: function(status,running) {
        if (status == "Failure")
            return running ? 'running_red.gif' : 'stopped_red.gif';
        if (status == "Success")
            return running ? 'running_green.gif' : 'stopped_green.gif';
        return '';
    },
    render: function() {
        var src = '/img/statuses/' + this.getImage(this.props.status,this.props.running);
        var alt = this.props.status + " " + (this.props.isRunning ? "in progress" : "completed");
        return (
            <img src={src} alt={alt} />
        );
    }
});

var AgentListItem = React.createClass({
    handleSelect: function() {
        this.props.onSelect(this.props.agent);
    },
    render: function() {
        return (
            <ListGroupItem>
            <input type="checkbox" checked={this.props.selected} onClick={this.handleSelect} className="agent__checkbox" />
            &nbsp;
            <AgentStatus running={this.props.agent.running} status={this.props.agent.status} />
            &nbsp;
            <span className="agent__text agent__name">{this.props.agent.name}</span>
            &nbsp;
            <span className="agent__text agent__status">[{this.props.agent.statusText}]</span>
            </ListGroupItem>
        );
    }
});

var SelectAllElement = React.createClass({
    render: function() {
        if (!this.props.visible)
            return null;
        return (
            <ListGroupItem>
            <input type="checkbox" onClick={this.props.onSelect} checked={this.props.checked} />
            &nbsp;
            All agents
            </ListGroupItem>
        );
    }
});

var AgentList = React.createClass({
    render: function() {
        if (this.props.showLoader)
            return (
                <div>
                <img src="/img/agent-list-loader.gif" alt="loading" />
                </div>
            );
        var selectedMap = {};
        for (var i = 0; i < this.props.selected.length; ++i) {
            selectedMap[this.props.selected[i]] = true;
        }
        var agentList = this.props.agents.map(function(item,index) {
            var selected = !!selectedMap[item.id];
            return (
                <AgentListItem agent={item} selected={selected} onSelect={this.props.onSelect} />
            );
        }.bind(this));

        var selectAllChecked = this.props.selected.length > 0;

        return (
            <div>
            <br/>
            <ListGroup>
            <SelectAllElement visible={this.props.agents.length > 0} onSelect={this.props.onSelectAll} checked={selectAllChecked} />
            {agentList}
            </ListGroup>
            </div>
        );
    }
});

var MultiActionToolbar = React.createClass({
    render: function() {
        if (!this.props.visible)
            return null;
        return (
            <ButtonToolbar>
            <Button disabled={!this.props.enabled} onClick={this.props.onStart}><Glyphicon glyph='play' /> Start</Button>
            <Button disabled={!this.props.enabled} onClick={this.props.onStop}><Glyphicon glyph='stop' /> Stop</Button>
            <Button disabled={!this.props.enabled} onClick={this.props.onReboot}><Glyphicon glyph='eject' /> Reboot</Button>
            <Button disabled={!this.props.enabled} onClick={this.props.onRunCustomBuild}><Glyphicon glyph='th' /> Clean&amp;Build</Button>
            </ButtonToolbar>
        );
    }
});

var InfoMessage = React.createClass({
    render: function() {
        if (String.isNullOrEmpty(this.props.message))
            return null;
        var dialogStyle={
            display: "block"
        };
        var modalHeaderStyle = {
            "border-bottom-width": "0px"
        };
        return (
            <div className="static-modal">
            <div tabindex="-1" role="dialog" style={dialogStyle} className="modal in">
            <div className="modal-dialog">
            <div className="modal-content">
            <div className="modal-header" style={modalHeaderStyle}>
            <button type="button" className="close" aria-hidden="true" onClick={this.props.onClose}>×</button>
            <h4 className="modal-title">{this.props.message}</h4>
            </div>
            </div>
            </div>
            </div>
            </div>
        );
    }
});

var HomePage = React.createClass({
    getInitialState: function() {
        return  {
            servers: [],
            selectedServerIndex: null,
            agents: [],
            selectedAgents: [],
            manuallySelectedAgents: [],
            message: null,
            showAgentListLoader: false
        };
    },
    onServerSelect: function(serverIndex) {
        if (this.state.selectedServerIndex == serverIndex)
            return;
        this.setState({
            showAgentListLoader: true,
            selectedServerIndex: serverIndex,
            selectedAgents: [],
            manuallySelectedAgents: [],
            agents: []
        });
        this.__resetPollAgentTimers();
        this.setState({
            pollAgentTimer: new BasedOnActivityIntervalTimer(
                this.loadAgents.bind(this, this.state.servers[serverIndex]),
                5000,
                60000,
                this.state.idleDetector)
        });
    },
    componentDidMount: function() {
        this.getServerList();
        var idleDetector = new IdleDetector();
        idleDetector.attachToDocument();
        this.setState({
            idleDetector: idleDetector
        });
    },
    componentUnmount: function() {
        this.__resetPollAgentTimers();
        this.state.idleDetector.detachFromDocument();
    },
    __resetPollAgentTimers: function() {
        if (this.state.pollAgentTimer) {
            this.state.pollAgentTimer.stop();
            this.setState({
                pollAgentTimer: null
            });
        }
    },
    getServerList: function() {
        $.get('/servers/list', function(response) {
            if (this.isMounted()) {
                var servers = response.servers;
                if (servers && servers.length > 0)
                    this.loadAgents(servers[0]);
                this.setState({
                    servers: servers,
                    selectedAgents: [],
                    agents: [],
                    manuallySelected: []
                });
                var defaultServerIndex = servers && servers.length > 0 ? 0 : null;
                if (defaultServerIndex !== null)
                    this.onServerSelect(defaultServerIndex);

            }
        }.bind(this));
    },
    loadAgents: function(server) {
        if (server == null)
            return;
        Console.log(new Date().toString() + ' request agents server=[' + server.id + '/' + server.alias + ']');
        $.get('/agents/list/' + server.id, function(response) {
            if (this.isMounted()) {
                this.setState({
                    showAgentListLoader: false,
                    agents: response.agents
                });
            }
        }.bind(this))
        .fail(function() {
            this.setState({
                showAgentListLoader: false,
                agents: [],
                selectedAgents: [],
                manuallySelectedAgents: []
            });
        }.bind(this));
    },
    handleSelectAgent: function(agent) {
        var manuallySelected = this.state.selectedAgents.slice();
        this.setState({
            selectedAgents: this.invertSelection(this.state.selectedAgents.slice(),agent.id),
            manuallySelectedAgents: this.invertSelection(manuallySelected.slice(),agent.id)
        });
    },
    invertSelection: function(list,itemId) {
        var pos = list.indexOf(itemId);
        var result = list.slice();
        if (pos != -1)
            result.splice(pos,1);
        else
            result.push(itemId);
        return result;
    },
    handleSelectAll: function() {
        var result = [];
        if (this.state.selectedAgents.length == 0) {
            if (this.state.manuallySelectedAgents.length == 0) {
                result = this.state.agents.map(function(agent) { return agent.id; });
            }
            else {
                result = this.state.manuallySelectedAgents.slice();
            }
        }
        else {
            if (this.state.selectedAgents.length < this.state.agents.length) {
                result = this.state.agents.map(function(agent) { return agent.id; });
            }
            else {
                result = [];
            }
        }
        this.setState({
            selectedAgents: result
        });
    },
    handleStartBuild: function() {
        this.execActionForAgents(
            '/agents/startBuild',
            'request to trigger build was sent',
            'build triggered');
    },
    handleStopBuild: function() {
        this.execActionForAgents('/agents/stopBuild',
            'request to stop build was sent',
            'build stopped');
    },
    handleRebootAgent: function() {
        this.execActionForAgents(
            '/agents/rebootAgent',
            'request to reboot agent was sent',
            'reboot triggered');
    },
    handleRunCustomBuild: function() {
        this.execActionForAgents(
            '/agents/runCustomBuild',
            'request to run custom build sent',
            'custom build has triggered');
    },
    showMessage: function(message) {
        if (this.__clearMessageTimeout) {
            clearTimeout(this.__clearMessageTimeout);
            delete this.__clearMessageTimeout;
        }
        this.setState({
            message: message
        });
        this.__clearMessageTimeout = setTimeout(function() {
            this.closeMessage();
        }.bind(this), 5000);
    },
    closeMessage: function() {
        this.setState({
            message: null
        });
    },
    execActionForAgents: function(url, triggerMessage, completeMessage) {
        this.showMessage(triggerMessage);
        $.post(url,
        { serverId: this.state.servers[this.state.selectedServerIndex].id,
            agentIds: this.state.selectedAgents },
            function (response) {
                console.log(response);
                this.showMessage(completeMessage);
            }.bind(this))
        .fail(function(response) {
            console.log(response);
        });;
    },
    render: function() {
        return (
            <p>
            <InfoMessage message={this.state.message} onClose={this.closeMessage}/>
            <ServerList servers={this.state.servers} selectedServerIndex={this.state.selectedServerIndex} onServerSelect={this.onServerSelect} />
            <Grid>
            <Row className="show-grid">
            <Col xs={12} md={6}>
            <br/>
            <MultiActionToolbar enabled={this.state.selectedAgents.length > 0} visible={this.state.agents.length > 0} onStart={this.handleStartBuild} onStop={this.handleStopBuild} onReboot={this.handleRebootAgent} onRunCustomBuild={this.handleRunCustomBuild}/>
            <AgentList agents={this.state.agents} selected={this.state.selectedAgents} onSelect={this.handleSelectAgent} onSelectAll={this.handleSelectAll} showLoader={this.state.showAgentListLoader}/>
            </Col>
            </Row>
            </Grid>
            </p>
        );
    }
});

React.render(
    <HomePage />,
    document.getElementById('main-content')
);
