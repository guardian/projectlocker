import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import StorageSelector from '../../Selectors/StorageSelector.jsx';

class DestinationStorageComponent extends CommonMultistepComponent {
    static propTypes = {
        storageList: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            isLoggedIn: false,
            currentUsername: "",
            isAdmin: false
        }
    }

    checkLogin(){
        this.setState({loading: true, haveChecked: true}, ()=>
            axios.get("/api/isLoggedIn")
                .then(response=>{ //200 response means we are logged in
                    this.setState({
                        isLoggedIn: true,
                        currentUsername: response.data.uid,
                        isAdmin: response.data.isAdmin
                    });
                })
                .catch(error=>{
                    this.setState({
                        isLoggedIn: false,
                        currentUsername: ""
                    })
                })
        );
    }

    componentWillMount(){
        this.checkLogin();
    }
    render(){
        return <div>
            <h3>Select destination storage</h3>
            <p>Lastly, we need to know where the project file should be stored</p>
            <table>
                <tbody>
                <tr>
                    <td>Storage</td>
                    <td><StorageSelector enabled={this.state.isAdmin}
                                         selectedStorage={this.props.selectedStorage}
                                         selectionUpdated={this.props.selectionUpdated}
                                         storageList={this.props.storageList}/></td>
                </tr>
                <tr style={{display: this.state.isAdmin ? "none": "inherit"}}>
                    <td></td>
                    <td><i>Non-admin users cannot change the storage location of projects</i></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default DestinationStorageComponent;
