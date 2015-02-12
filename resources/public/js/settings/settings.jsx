var Table = ReactBootstrap.Table;
var Button = ReactBootstrap.Button;
var ButtonToolbar = ReactBootstrap.ButtonToolbar;
var Input = ReactBootstrap.Input;
var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Modal = ReactBootstrap.Modal;

var Server = React.createClass({
    handleDelete: function() {
        this.props.onDelete(this.props.server);
    },
    render: function() {
        return (
            <tr>
            <td>{this.props.index+1}</td>
            <td>{this.props.server.alias}</td>
            <td>{this.props.server.host}:{this.props.server.port}</td>
            <td>{this.props.server.project}</td>
            <td>
            <ButtonToolbar>
            <Button bsStyle="danger" onClick={this.handleDelete}>Delete</Button>
            </ButtonToolbar>
            </td>
            </tr>
        );
    }
});

var ServerList = React.createClass({
    render: function() {
        var servers = this.props.servers.map(function (server, index) {
            return (
                <Server index={index} server={server} onDelete={this.props.onDelete} />
            );
        }.bind(this));
        return (
            <Table striped bordered condensed hover>
            <thead>
            <colgroup>
            <col span="1" width="5%" />
            <col span="1" width="35%" />
            <col span="1" width="20%" />
            <col span="1" width="20%" />
            <col span="1" width="20%" />
            </colgroup>
            <tr>
            <th>#</th>
            <th>Alias</th>
            <th>Host:Port</th>
            <th>Project</th>
            <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {servers}
            </tbody>
            </Table>
        );
    }
});

var AddServerForm = React.createClass({
    render: function() {
        return (
            <div>
            <Button onClick={this.props.onBeginAddServer}>Add</Button>
            </div>
        );
    }
});

var DeleteConfimationDialog = React.createClass({
    handleOk: function() {
        this.props.onOk(this.props.server);
    },
    render: function() {
        var title = "Do you really want to delete '" + this.props.server.alias + "'?";
    return (
        <Modal title={title} backdrop={false} animation={false} onRequestHide={this.props.onHide}>
        <div className="modal-body">
        {this.props.server.alias} at {this.props.server.host}:{this.props.server.port}
        </div>
        <div className="modal-footer">
        <Button onClick={this.props.onCancel}>Cancel</Button>
        <Button bsStyle="danger" onClick={this.handleOk}>Delete</Button>
        </div>
        </Modal>
    );
}
});

var SettingsPage = React.createClass({
    getInitialState: function() {
        return {
            showList: true,
            servers: []
        };
    },
    beginAddServer: function() {
        this.setState({
            showList:false
        });
    },
    showList: function() {
        this.setState({
            showList:true
        });
    },
    saveServer: function(server) {
        $.post("/settings/servers/add",server,function() {
            this.loadServerList();
            this.showList();
        }.bind(this));
    },
    cancelEditServer: function() {
        this.showList();
    },
    loadServerList: function() {
        $.get('/settings/servers/list', function(response) {
            if (this.isMounted()) {
                this.setState({
                    servers: response.servers
                });
            }
        }.bind(this));
    },
    componentDidMount: function() {
        this.loadServerList();
    },
    handleDelete: function(server) {
        $.post('/settings/servers/delete', {id:server.id}, function (response) {
            this.hideDeleteConfirmationDialog();
            this.loadServerList();
        }.bind(this));
    },
    confirmDelete: function(server) {
        this.setState({
            showList: true,
            servers: this.state.servers,
            serverToDelete: server
        });
    },
    hideDeleteConfirmationDialog: function() {
        this.setState({
            serverToDelete: null
        });
    },
    cancelDelete: function() {
        this.hideDeleteConfirmationDialog();
    },
    render: function() {
        if (this.state.showList)
            return (
                <div>
                <AddServerForm onBeginAddServer={this.beginAddServer} />
                <br/>
                <ServerList servers={this.state.servers} onDelete={this.confirmDelete} />
                {this.state.serverToDelete != null
                    ? <DeleteConfimationDialog
                    server={this.state.serverToDelete}
                    onCancel={this.cancelDelete}
                    onOk={this.handleDelete}
                    onRequestHide={this.cancelDelete} />
                    : null}
                </div>
            );
        return (
            <div>
            <EditServerForm onSave={this.saveServer} onCancel={this.cancelEditServer}/>
            </div>
        );
    }
});

var EditServerForm = React.createClass({
    handleSubmit: function(e) {
        e.preventDefault();
        this.props.onSave({
            alias: this.refs.alias.getInputDOMNode().value.trim(),
            host: this.refs.host.getInputDOMNode().value.trim(),
            port: this.refs.port.getInputDOMNode().value.trim(),
            project: this.refs.project.getInputDOMNode().value.trim(),
            username: this.refs.username.getInputDOMNode().value.trim(),
            password: this.refs.password.getInputDOMNode().value.trim()
        });
    },
    cancelEdit: function() {
        this.props.onCancel();
    },
    render: function() {
        return (
            <form action="" method="POST" onSubmit={this.handleSubmit}>
            <Grid>
            <Row>
            <Col xs={12} md={6}>

            <Input type="text" label='Alias' ref="alias" placeholder="Enter text" />
            <Input type="text" label='Host' ref="host" placeholder="Enter text" />
            <Input type="text" label='Port' ref="port" placeholder="Enter text" />
            <Input type="text" label='Project' ref="project" placeholder="Enter text" />
            <Input type="text" label='Username' ref="username" placeholder="Enter text" />
            <Input type="text" label='Password' ref="password" placeholder="Enter text" />
            <ButtonToolbar>
            <Button bsStyle="success" type="submit">Save</Button>
            <Button onClick={this.cancelEdit}>Cancel</Button>
            </ButtonToolbar>
            </Col>
            </Row>
            </Grid>
            </form>
        );
    }
});

React.render(
    <SettingsPage />,
    document.getElementById('main-content')
);

