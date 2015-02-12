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

var AgentListItem = React.createClass({
    handleSelect: function() {
        this.props.onSelect(this.props.agent);
    },
    render: function() {
        return (
            <ListGroupItem>
            <Input type="checkbox" label={this.props.agent.name} checked={this.props.selected} onClick={this.handleSelect} />
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
            <Input type="checkbox" label={'All agents'} onClick={this.props.onSelect} />
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
        return (
            <div>
            <br/>
            <ListGroup>
            <SelectAllElement visible={this.props.agents.length > 0} onSelect={this.props.onSelectAll}/>
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
            selectedAgents: []
        };
    },
    onServerSelect: function(serverIndex) {
        if (this.state.selectedServerIndex == serverIndex)
            return;
        this.setState({
            selectedServerIndex: serverIndex
        });
        this.loadAgents(this.state.servers[serverIndex]);
    },
    componentDidMount: function() {
        this.getServerList();
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
        var alreadySelected = this.state.selectedAgents.indexOf(agent.id) != -1;
        var newSelectedAgents = this.state.selectedAgents;
        var agentIndex = this.state.selectedAgents.indexOf(agent.id);
        if (agentIndex != -1)
            newSelectedAgents.splice(agentIndex,1);
        else
            newSelectedAgents.push(agent.id);
        this.setState({
            selectedAgents: newSelectedAgents
        });
    },
    handleSelectAll: function() {
        var m = {};
        for (var i = 0; i < this.state.selectedAgents.length; ++i) {
            m[this.state.selectedAgents[i]]=true;
        }
        var result = [];
        for (var i = 0; i < this.state.agents.length; ++i) {
            var agent = this.state.agents[i];
            if (!m[agent.id])
                result.push(agent.id);
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
                alert(successMessage);
            });
    },
    getServerList: function() {
        $.get('/servers/list', function(response) {
            if (this.isMounted()) {
                var servers = response.servers;
                if (servers && servers.length > 0)
                    this.loadAgents(servers[0]);
                this.setState({
                    servers: servers,
                    selectedServerIndex: servers && servers.length > 0
                    ? 0
                    : null,
                    selectedAgents: [],
                    agents: []
                });
            }
        }.bind(this));
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
