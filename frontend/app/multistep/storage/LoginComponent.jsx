import React from 'react';

class StorageLoginComponent extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            hostname: "",
            port: "",
            username: "",
            password: ""
        }
    }

    componentDidUpdate(prevProps,prevState){
        if(prevState!=this.state) this.props.valueWasSet(this.state);
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];

        if(!selectedStorage.needsLogin){
            return(
                <div>
                    <h3>Storage Login</h3>
                    <p>{selectedStorage.name} storage does not require login.</p>
                </div>
            )
        } else {
            return (
                <div>
                    <h3>Storage Login</h3>
                    <p>{selectedStorage.name} is a remote storage.  Please supply the information we need to log in to it.  If any field
                        is not applicable, leave it blank
                    </p>
                    <form>
                        <table>
                            <tbody>
                            <tr>
                                <td>Storage host (or bucket name)</td>
                                <td><input id="hostname_input" value={this.state.hostname} onChange={(event)=>this.setState({hostname: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td>Storage port (if applicable)</td>
                                <td><input id="port_input" value={this.state.port} onChange={(event)=>this.setState({port: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td>User name</td>
                                <td><input id="username_input" value={this.state.username} onChange={(event)=>this.setState({username: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td>Password</td>
                                <td><input id="password_input" type="password" value={this.state.password} onChange={(event)=>this.setState({password: event.target.value})}/></td>
                            </tr>
                            </tbody>
                        </table>
                    </form>
                </div>)
        }
    }
}

export default StorageLoginComponent;
