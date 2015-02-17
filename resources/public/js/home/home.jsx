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
            <input type="checkbox" checked={this.props.selected} onClick={this.handleSelect} />
            &nbsp;
            <AgentStatus running={this.props.agent.running} status={this.props.agent.status} />
            &nbsp;
            <span>{this.props.agent.name}</span>

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
        var partiallySelected = this.props.selected.length > 0 && this.props.selected.length < this.props.agents.length;

        return (
            <div>
            <br/>
            <ListGroup>
            <SelectAllElement visible={this.props.agents.length > 0} onSelect={this.props.onSelectAll} checked={this.props.checked} partiallyChecked={this.props.partiallyChecked} checked={selectAllChecked} partiallySelected={partiallySelected}/>
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
            </ButtonToolbar>
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
            manuallySelectedAgents: []
        };
    },
    onServerSelect: function(serverIndex) {
        if (this.state.selectedServerIndex == serverIndex)
            return;
        this.setState({
            selectedServerIndex: serverIndex,
            selectedAgents: [],
            manuallySelectedAgents: [],
            agents: []
        });
        this.loadAgents(this.state.servers[serverIndex]);
    },
    componentDidMount: function() {
        this.getServerList();
    },
    getServerList: function() {
        $.get('/servers/list', function(response) {
            if (this.isMounted()) {
                var servers = response.servers;
                if (servers && servers.length > 0)
                    this.loadAgents(servers[0]);
                this.setState({
                    servers: servers,
                    selectedServerIndex: servers && servers.length > 0 ? 0 : null,
                    selectedAgents: [],
                    agents: [],
                    manuallySelected: []
                });
            }
        }.bind(this));
    },
    loadAgents: function(server) {
        if (server == null)
            return;
        $.get('/agents/list/' + server.id, function(response) {
            if (this.isMounted()) {
                var agents = response.agents;
                this.setState({
                    agents: agents,
                    selectedAgents: []
                });
            }
        }.bind(this))
        .fail(function() {
            this.setState({
                agents: [],
                selectedAgents: []
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
        this.execActionForAgents('/agents/startBuild','build triggered');
    },
    handleStopBuild: function() {
        this.execActionForAgents('/agents/stopBuild','build stopped');
    },
    handleRebootAgent: function() {
        this.execActionForAgents('/agents/rebootAgent','reboot triggered');
    },
    execActionForAgents: function(url, successMessage) {
        $.post(url,
        { serverId: this.state.servers[this.state.selectedServerIndex].id,
            agentIds: this.state.selectedAgents },
            function (response) {
                console.log(response);
                alert(successMessage);
            })
        .fail(function(response) {
            console.log(response);
        });;
    },
    render: function() {
        return (
            <p>
            <ServerList servers={this.state.servers} selectedServerIndex={this.state.selectedServerIndex} onServerSelect={this.onServerSelect} />
            <Grid>
            <Row className="show-grid">
            <Col xs={12} md={6}>
            <br/>
            <MultiActionToolbar enabled={this.state.selectedAgents.length > 0} visible={this.state.agents.length > 0} onStart={this.handleStartBuild} onStop={this.handleStopBuild} onReboot={this.handleRebootAgent}/>
            <AgentList agents={this.state.agents} selected={this.state.selectedAgents} onSelect={this.handleSelectAgent} onSelectAll={this.handleSelectAll}/>
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
