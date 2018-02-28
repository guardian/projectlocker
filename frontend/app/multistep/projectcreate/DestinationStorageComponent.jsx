import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class DestinationStorageComponent extends CommonMultistepComponent {
    static propTypes = {
        storageList: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);
    }

    render(){
        return <div>
            <h3>Select destination storage</h3>
            <p>Lastly, we need to know where the project file should be stored</p>
            <table>
                <tbody>
                <tr>
                    <td>Storage</td>
                    <td><select id="storageSelector" value={this.props.selectedStorage}
                                onChange={event=>this.props.selectionUpdated(event.target.value)}>
                        {
                            this.props.storageList.map(storage=><option key={storage.id} value={storage.id}>{storage.rootpath} on {storage.storageType}</option>)
                        }
                    </select></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default DestinationStorageComponent;
